package com.trokr.model.state.contraproposta;
import  com.trokr.model.ContraProposta;
public interface EstadoContraProposta {
    default void solicitarAnalise(ContraProposta contraproposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void enviarParaHomologacao(ContraProposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void recusarParaHomologacao(ContraProposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void aceitar(ContraProposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void cancelar(ContraProposta contraproposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void voltar(ContraProposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    

    default void aceitarContraproposta(Proposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void negociacaoFalhou(Proposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }
    default void finalizarAcordo(Proposta proposta) { throw new IllegalStateException("Ação inválida neste estado."); }


}
